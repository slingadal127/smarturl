import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score
from sklearn.preprocessing import StandardScaler
import tensorflow as tf
from tensorflow import keras
from app.features import extract_features
import os

DATA_PATH = os.path.join(os.path.dirname(__file__), '..', 'data', 'phishing_site_urls.csv')


class URLClassifier:
    """
    Dual-model URL classifier — Random Forest + Neural Network.
    Both models trained on the same dataset for fair comparison.
    """

    def __init__(self):
        self.rf_model = None
        self.nn_model = None
        self.scaler   = StandardScaler()  # Normalizes features for NN
        self.is_trained = False
        self.stats = {}
        self._train()

    def _load_data(self):
        """Loads and preprocesses the Kaggle phishing dataset"""
        print("📂 Loading dataset...")
        df = pd.read_csv(DATA_PATH)

        # Clean column names
        df.columns = df.columns.str.strip()

        # Convert labels: "bad" → 1 (malicious), "good" → 0 (safe)
        df['label'] = (df['Label'].str.lower().str.strip() == 'bad').astype(int)

        # Drop rows with missing URLs
        df = df.dropna(subset=['URL'])

        print(f"✅ Loaded {len(df)} URLs ({df['label'].sum()} malicious, "
              f"{(df['label'] == 0).sum()} safe)")
        return df

    def _extract_all_features(self, urls):
        """Extracts features for all URLs — shows progress every 10k"""
        features = []
        for i, url in enumerate(urls):
            if i % 10000 == 0 and i > 0:
                print(f"   Processed {i}/{len(urls)} URLs...")
            try:
                features.append(extract_features(str(url)))
            except Exception:
                features.append(np.zeros(13))
        return np.array(features)

    def _train(self):
        """Trains both models on the phishing dataset"""
        df = self._load_data()

        # Use 50k samples for speed — enough for good accuracy
        sample_size = min(50000, len(df))
        df = df.sample(n=sample_size, random_state=42)

        print("🔧 Extracting features...")
        X = self._extract_all_features(df['URL'].values)
        y = df['label'].values

        # Split into train and test sets
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42, stratify=y
        )

        self.stats['training_samples'] = len(X_train)
        self.stats['test_samples']     = len(X_test)

        # Train Random Forest
        self._train_rf(X_train, X_test, y_train, y_test)

        # Scale features for Neural Network
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled  = self.scaler.transform(X_test)

        # Train Neural Network
        self._train_nn(X_train_scaled, X_test_scaled, y_train, y_test)

        self.is_trained = True
        print("🎉 Both models trained successfully!")
        self._print_comparison()

    def _train_rf(self, X_train, X_test, y_train, y_test):
        """Trains and evaluates the Random Forest model"""
        print("🌲 Training Random Forest...")
        self.rf_model = RandomForestClassifier(
            n_estimators=100,
            max_depth=20,
            min_samples_split=5,
            random_state=42,
            n_jobs=-1  # Use all CPU cores
        )
        self.rf_model.fit(X_train, y_train)
        y_pred = self.rf_model.predict(X_test)

        self.stats['rf_accuracy']  = round(accuracy_score(y_test, y_pred), 4)
        self.stats['rf_precision'] = round(precision_score(y_test, y_pred), 4)
        self.stats['rf_recall']    = round(recall_score(y_test, y_pred), 4)
        self.stats['rf_f1']        = round(f1_score(y_test, y_pred), 4)
        print(f"✅ RF Accuracy: {self.stats['rf_accuracy']:.2%}")

    def _train_nn(self, X_train, X_test, y_train, y_test):
        """Trains and evaluates the Neural Network model"""
        print("🧠 Training Neural Network...")
        self.nn_model = keras.Sequential([
            keras.layers.Input(shape=(13,)),
            keras.layers.Dense(64, activation='relu'),
            keras.layers.Dropout(0.3),      # Prevents overfitting
            keras.layers.Dense(32, activation='relu'),
            keras.layers.Dropout(0.2),
            keras.layers.Dense(1, activation='sigmoid')  # Binary output
        ])

        self.nn_model.compile(
            optimizer='adam',
            loss='binary_crossentropy',
            metrics=['accuracy']
        )

        # Early stopping — stops training when validation stops improving
        early_stop = keras.callbacks.EarlyStopping(
            monitor='val_loss',
            patience=3,
            restore_best_weights=True
        )

        self.nn_model.fit(
            X_train, y_train,
            epochs=20,
            batch_size=256,
            validation_split=0.1,
            callbacks=[early_stop],
            verbose=0  # Silent training
        )

        y_pred_prob = self.nn_model.predict(X_test, verbose=0).flatten()
        y_pred = (y_pred_prob >= 0.5).astype(int)

        self.stats['nn_accuracy']  = round(accuracy_score(y_test, y_pred), 4)
        self.stats['nn_precision'] = round(precision_score(y_test, y_pred), 4)
        self.stats['nn_recall']    = round(recall_score(y_test, y_pred), 4)
        self.stats['nn_f1']        = round(f1_score(y_test, y_pred), 4)
        print(f"✅ NN Accuracy: {self.stats['nn_accuracy']:.2%}")

    # Known safe domains — always return safe regardless of model output
    SAFE_DOMAINS = {
        'google.com', 'youtube.com', 'facebook.com', 'twitter.com',
        'instagram.com', 'linkedin.com', 'github.com', 'stackoverflow.com',
        'amazon.com', 'apple.com', 'microsoft.com', 'wikipedia.org',
        'reddit.com', 'netflix.com', 'spotify.com', 'anthropic.com'
    }

    def predict(self, url: str) -> dict:
        """
        Runs both models on a single URL and returns combined result.
        Uses allowlist for known safe domains, then ML for everything else.
        """
        # Check allowlist first — known safe domains always pass
        try:
            from urllib.parse import urlparse
            parsed = urlparse(url if url.startswith('http') else 'http://' + url)
            domain = parsed.netloc.replace('www.', '')
            if domain in self.SAFE_DOMAINS:
                return {
                    'rf_prediction':  False,
                    'rf_confidence':  0.01,
                    'nn_prediction':  False,
                    'nn_confidence':  0.01,
                    'is_malicious':   False,
                    'final_verdict':  'SAFE',
                }
        except Exception:
            pass

        features = extract_features(url).reshape(1, -1)
        features_scaled = self.scaler.transform(features)

        # Random Forest prediction
        rf_prob = self.rf_model.predict_proba(features)[0][1]
        rf_pred = rf_prob >= 0.7

        # Neural Network prediction
        nn_prob = float(self.nn_model.predict(features_scaled, verbose=0)[0][0])
        nn_pred = nn_prob >= 0.7

        # Final verdict — BOTH models must agree
        is_malicious = rf_pred and nn_pred

        return {
            'rf_prediction':  bool(rf_pred),
            'rf_confidence':  round(rf_prob, 4),
            'nn_prediction':  bool(nn_pred),
            'nn_confidence':  round(nn_prob, 4),
            'is_malicious':   is_malicious,
            'final_verdict':  'MALICIOUS' if is_malicious else 'SAFE',
        }

    def _print_comparison(self):
        """Prints a side-by-side model comparison table"""
        print("\n📊 Model Comparison:")
        print(f"{'Metric':<12} {'Random Forest':>15} {'Neural Network':>15}")
        print("-" * 44)
        for m in ['accuracy', 'precision', 'recall', 'f1']:
            print(f"{m:<12} {self.stats[f'rf_{m}']:>15.2%} "
                  f"{self.stats[f'nn_{m}']:>15.2%}")


# Single instance — trained once at startup
classifier = URLClassifier()