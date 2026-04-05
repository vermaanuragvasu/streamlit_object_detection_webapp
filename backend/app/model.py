"""
Object Detection Model Module

Loads the FasterRCNN ResNet50 FPN V2 model and provides prediction utilities.
The model is loaded once at startup for optimal performance.
"""

import threading
from typing import Any

import torch
from torchvision.models.detection import (
    FasterRCNN_ResNet50_FPN_V2_Weights,
    fasterrcnn_resnet50_fpn_v2,
)

_model = None
_model_lock = threading.Lock()

weights = FasterRCNN_ResNet50_FPN_V2_Weights.DEFAULT
CATEGORIES: list[str] = weights.meta["categories"]
img_preprocess = weights.transforms()


def get_device() -> torch.device:
    """Return the best available device (CUDA > CPU)."""
    if torch.cuda.is_available():
        return torch.device("cuda")
    return torch.device("cpu")


def load_model() -> torch.nn.Module:
    """Load the FasterRCNN model (singleton, thread-safe)."""
    global _model
    if _model is None:
        with _model_lock:
            if _model is None:
                device = get_device()
                _model = fasterrcnn_resnet50_fpn_v2(
                    weights=weights, box_score_thresh=0.5
                )
                _model.eval()
                _model.to(device)
    return _model


def predict_image(img_tensor: torch.Tensor) -> dict[str, Any]:
    """
    Run object detection on a preprocessed image tensor.

    Args:
        img_tensor: Image tensor of shape (3, H, W) with values 0-255.

    Returns:
        Dictionary with 'boxes', 'labels', 'scores' keys.
    """
    model = load_model()
    device = get_device()

    img_processed = img_preprocess(img_tensor)
    img_batch = img_processed.unsqueeze(0).to(device)

    with torch.no_grad():
        predictions = model(img_batch)

    prediction = predictions[0]

    boxes = prediction["boxes"].cpu().tolist()
    scores = prediction["scores"].cpu().tolist()
    labels = [CATEGORIES[label] for label in prediction["labels"].cpu().tolist()]

    return {
        "boxes": boxes,
        "labels": labels,
        "scores": scores,
    }
