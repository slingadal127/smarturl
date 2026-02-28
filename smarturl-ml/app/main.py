from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from app.schemas import ClassifyRequest, ClassifyResponse, ModelStatsResponse
from app.models import classifier
from app.features import get_triggered_features

app = FastAPI(
    title="SmartURL ML Service",
    description="Dual-model malicious URL classifier — Random Forest + Neural Network",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8082", "http://localhost:3000"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {
        "status": "running",
        "model_trained": classifier.is_trained
    }


@app.post("/ml/classify", response_model=ClassifyResponse)
def classify(request: ClassifyRequest):
    """
    Classifies a URL as safe or malicious using both RF and NN models.
    Called by Spring Boot before shortening any URL.
    """
    if not classifier.is_trained:
        raise HTTPException(status_code=503, detail="Models not trained yet.")

    result = classifier.predict(request.url)
    triggered = get_triggered_features(request.url)

    return ClassifyResponse(
        url=request.url,
        is_malicious=result['is_malicious'],
        rf_prediction=result['rf_prediction'],
        rf_confidence=result['rf_confidence'],
        nn_prediction=result['nn_prediction'],
        nn_confidence=result['nn_confidence'],
        final_verdict=result['final_verdict'],
        triggered_features=triggered
    )


@app.get("/ml/stats", response_model=ModelStatsResponse)
def stats():
    """Returns accuracy comparison between RF and NN models"""
    if not classifier.is_trained:
        raise HTTPException(status_code=503, detail="Models not trained yet.")
    return ModelStatsResponse(**classifier.stats)