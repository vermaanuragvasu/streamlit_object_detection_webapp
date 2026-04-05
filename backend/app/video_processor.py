"""
Video processing module for frame-by-frame object detection.

Handles video upload, frame extraction, detection, bounding box drawing,
and output video generation with performance optimizations.
"""

import os
import time
from typing import Any

import cv2
import numpy as np
import torch
from torchvision.utils import draw_bounding_boxes

from .model import CATEGORIES, img_preprocess, load_model, get_device

MAX_FRAME_DIMENSION = 640
BATCH_SIZE = 4


def resize_frame(frame: np.ndarray, max_dim: int = MAX_FRAME_DIMENSION) -> np.ndarray:
    """Resize frame while maintaining aspect ratio."""
    h, w = frame.shape[:2]
    if max(h, w) <= max_dim:
        return frame
    scale = max_dim / max(h, w)
    new_w = int(w * scale)
    new_h = int(h * scale)
    return cv2.resize(frame, (new_w, new_h), interpolation=cv2.INTER_LINEAR)


def draw_boxes_on_frame(
    frame: np.ndarray, prediction: dict[str, Any]
) -> np.ndarray:
    """Draw bounding boxes on a frame using detection results."""
    if len(prediction["boxes"]) == 0:
        return frame

    frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    img_tensor = torch.tensor(frame_rgb).permute(2, 0, 1).to(torch.uint8)

    boxes = torch.tensor(prediction["boxes"], dtype=torch.float32)
    labels = prediction["labels"]
    colors = ["red" if label == "person" else "green" for label in labels]

    label_strings = [
        f"{label}: {score:.2f}"
        for label, score in zip(labels, prediction["scores"])
    ]

    img_with_boxes = draw_bounding_boxes(
        img_tensor, boxes=boxes, labels=label_strings, colors=colors, width=2
    )
    result = img_with_boxes.detach().numpy().transpose(1, 2, 0)
    return cv2.cvtColor(result, cv2.COLOR_RGB2BGR)


def process_batch(
    frames: list[np.ndarray], model: torch.nn.Module, device: torch.device
) -> list[dict[str, Any]]:
    """Process a batch of frames through the model."""
    processed = []
    for frame in frames:
        frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        img_tensor = torch.tensor(frame_rgb, dtype=torch.uint8).permute(2, 0, 1)
        img_processed = img_preprocess(img_tensor)
        processed.append(img_processed)

    batch = torch.stack(processed).to(device)

    with torch.no_grad():
        predictions = model(batch)

    results = []
    for pred in predictions:
        boxes = pred["boxes"].cpu().tolist()
        scores = pred["scores"].cpu().tolist()
        labels = [CATEGORIES[label] for label in pred["labels"].cpu().tolist()]
        results.append({"boxes": boxes, "labels": labels, "scores": scores})

    return results


def process_video(
    input_path: str, output_dir: str
) -> dict[str, Any]:
    """
    Process a video file with frame-by-frame object detection.

    Args:
        input_path: Path to input video file.
        output_dir: Directory to save processed video.

    Returns:
        Dictionary with processing results and output video path.
    """
    model = load_model()
    device = get_device()

    cap = cv2.VideoCapture(input_path)
    if not cap.isOpened():
        raise ValueError(f"Could not open video: {input_path}")

    fps = cap.get(cv2.CAP_PROP_FPS)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

    # Determine output dimensions after resize
    sample_frame = np.zeros((height, width, 3), dtype=np.uint8)
    resized_sample = resize_frame(sample_frame)
    out_h, out_w = resized_sample.shape[:2]

    output_filename = f"detected_{int(time.time())}.mp4"
    output_path = os.path.join(output_dir, output_filename)

    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out_writer = cv2.VideoWriter(output_path, fourcc, fps, (out_w, out_h))

    frame_detections = []
    processed_count = 0
    start_time = time.time()

    frames_buffer = []
    frame_indices = []

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        frame = resize_frame(frame)
        frames_buffer.append(frame)
        frame_indices.append(processed_count)

        if len(frames_buffer) >= BATCH_SIZE:
            batch_results = process_batch(frames_buffer, model, device)
            for i, (frm, det) in enumerate(zip(frames_buffer, batch_results)):
                annotated = draw_boxes_on_frame(frm, det)
                out_writer.write(annotated)
                frame_detections.append(
                    {
                        "frame": frame_indices[i],
                        "detections": [
                            {
                                "label": label,
                                "confidence": score,
                                "bbox": box,
                            }
                            for label, score, box in zip(
                                det["labels"], det["scores"], det["boxes"]
                            )
                        ],
                    }
                )
            processed_count += len(frames_buffer)
            frames_buffer = []
            frame_indices = []

    # Process remaining frames
    if frames_buffer:
        batch_results = process_batch(frames_buffer, model, device)
        for i, (frm, det) in enumerate(zip(frames_buffer, batch_results)):
            annotated = draw_boxes_on_frame(frm, det)
            out_writer.write(annotated)
            frame_detections.append(
                {
                    "frame": frame_indices[i],
                    "detections": [
                        {
                            "label": label,
                            "confidence": score,
                            "bbox": box,
                        }
                        for label, score, box in zip(
                            det["labels"], det["scores"], det["boxes"]
                        )
                    ],
                }
            )
        processed_count += len(frames_buffer)

    cap.release()
    out_writer.release()

    processing_time = time.time() - start_time

    return {
        "total_frames": total_frames,
        "processed_frames": processed_count,
        "fps": round(processed_count / processing_time, 2) if processing_time > 0 else 0,
        "processing_time_seconds": round(processing_time, 2),
        "output_path": output_path,
        "output_filename": output_filename,
        "frame_detections": frame_detections,
    }
