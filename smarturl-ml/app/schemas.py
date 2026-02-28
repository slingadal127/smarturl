from pydantic import BaseModel
from typing import Optional

class ClassifyRequest(BaseModel):
    url: str

class ClassifyResponse(BaseModel):
    url: str
    is_malicious: bool

    # Random Forest results
    rf_prediction: bool
    rf_confidence: float

    # Neural Network results
    nn_prediction: bool
    nn_confidence: float

    # Final verdict — malicious if either model flags it
    final_verdict: str  # "SAFE" or "MALICIOUS"

    # Which features triggered the classification
    triggered_features: list[str]

class ModelStatsResponse(BaseModel):
    rf_accuracy: float
    rf_precision: float
    rf_recall: float
    rf_f1: float
    nn_accuracy: float
    nn_precision: float
    nn_recall: float
    nn_f1: float
    training_samples: int
    test_samples: int