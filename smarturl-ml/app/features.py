import re
import numpy as np
from urllib.parse import urlparse

# Suspicious words commonly found in phishing URLs
SUSPICIOUS_WORDS = [
    'login', 'verify', 'account', 'update', 'secure', 'banking',
    'confirm', 'password', 'credit', 'signin', 'ebayisapi', 'webscr',
    'paypal', 'submit', 'redirect', 'dispatch', 'cmd', 'checkout'
]

# High-risk TLDs commonly used in phishing
SUSPICIOUS_TLDS = [
    '.xyz', '.top', '.click', '.link', '.work', '.party',
    '.gq', '.ml', '.cf', '.ga', '.tk', '.pw'
]

# Feature names — used for triggered_features in response
FEATURE_NAMES = [
    'url_length',
    'num_dots',
    'num_hyphens',
    'num_at_symbols',
    'num_digits',
    'has_ip_address',
    'has_https',
    'domain_length',
    'path_length',
    'num_subdomains',
    'has_suspicious_words',
    'special_char_count',
    'tld_suspicious',
]


def extract_features(url: str) -> np.ndarray:
    """
    Extracts 13 numerical features from a URL string.
    These features are the input to both ML models.
    """
    # Add scheme if missing so urlparse works correctly
    if not url.startswith('http'):
        url = 'http://' + url

    try:
        parsed = urlparse(url)
        domain = parsed.netloc or ''
        path   = parsed.path or ''
    except Exception:
        domain = ''
        path   = ''

    url_lower = url.lower()

    # Feature 1: Total URL length — phishing URLs tend to be very long
    url_length = len(url)

    # Feature 2: Number of dots — many dots = suspicious subdomains
    num_dots = url.count('.')

    # Feature 3: Number of hyphens — hyphens common in fake domains
    num_hyphens = url.count('-')

    # Feature 4: @ symbol — tricks browsers into ignoring the real domain
    # e.g. http://google.com@evil.com actually goes to evil.com
    num_at = url.count('@')

    # Feature 5: Digit count — high digits = suspicious
    num_digits = sum(c.isdigit() for c in url)

    # Feature 6: IP address instead of domain name — classic phishing trick
    has_ip = 1 if re.search(r'\d+\.\d+\.\d+\.\d+', domain) else 0

    # Feature 7: HTTPS — weak signal but still useful
    has_https = 1 if url_lower.startswith('https') else 0

    # Feature 8: Domain length — very long domains are suspicious
    domain_length = len(domain)

    # Feature 9: Path length — long paths common in phishing URLs
    path_length = len(path)

    # Feature 10: Number of subdomains — many subdomains = suspicious
    num_subdomains = max(0, len(domain.split('.')) - 2)

    # Feature 11: Suspicious words in URL
    has_suspicious = 1 if any(w in url_lower for w in SUSPICIOUS_WORDS) else 0

    # Feature 12: Special character count (%, =, ?, &, !)
    special_chars = sum(url.count(c) for c in ['%', '=', '?', '&', '!', '+'])

    # Feature 13: Suspicious TLD
    tld_suspicious = 1 if any(url_lower.endswith(t) or t + '/' in url_lower
                               for t in SUSPICIOUS_TLDS) else 0

    return np.array([
        url_length, num_dots, num_hyphens, num_at, num_digits,
        has_ip, has_https, domain_length, path_length, num_subdomains,
        has_suspicious, special_chars, tld_suspicious
    ], dtype=float)


def get_triggered_features(url: str) -> list[str]:
    """
    Returns human-readable list of suspicious features found in the URL.
    Used in the API response to explain why a URL was flagged.
    """
    if not url.startswith('http'):
        url = 'http://' + url

    triggered = []
    url_lower = url.lower()

    try:
        parsed = urlparse(url)
        domain = parsed.netloc or ''
    except Exception:
        domain = ''

    if len(url) > 100:
        triggered.append(f"Very long URL ({len(url)} chars)")
    if url.count('.') > 4:
        triggered.append(f"Many dots ({url.count('.')})")
    if url.count('-') > 3:
        triggered.append(f"Many hyphens ({url.count('-')})")
    if '@' in url:
        triggered.append("Contains @ symbol")
    if re.search(r'\d+\.\d+\.\d+\.\d+', domain):
        triggered.append("IP address instead of domain")
    if not url_lower.startswith('https'):
        triggered.append("No HTTPS")
    words = [w for w in SUSPICIOUS_WORDS if w in url_lower]
    if words:
        triggered.append(f"Suspicious words: {', '.join(words[:3])}")
    if any(url_lower.endswith(t) or t + '/' in url_lower for t in SUSPICIOUS_TLDS):
        triggered.append("High-risk TLD")

    return triggered if triggered else ["No specific triggers — statistical pattern"]