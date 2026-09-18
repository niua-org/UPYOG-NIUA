import os
import logging
from flask import Blueprint, send_from_directory

logger = logging.getLogger(__name__)

static_bp = Blueprint("static_routes", __name__)

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


@static_bp.route("/")
@static_bp.route("/upyog-voice-bot", strict_slashes=False)
@static_bp.route("/upyog-voice-bot/")
def index_page():
    """
    Serves the chatbot UI (index.html).
    Route '/' handles direct local access at localhost:8090.
    Route '/upyog-voice-bot' handles requests routed through niautt's EKS ingress
    at niautt.niua.in/upyog-voice-bot.
    strict_slashes=False accepts both trailing-slash and non-trailing-slash URLs.
    """
    return send_from_directory(BASE_DIR, 'index.html')


@static_bp.route("/assets/<path:filename>")
@static_bp.route("/upyog-voice-bot/assets/<path:filename>")
@static_bp.route("/upyog-voice/assets/<path:filename>")
def serve_assets(filename):
    """
    Serves static files from the assets/ folder (styles.css, constants.js, icons.js).
    Three route aliases match all deployment paths — local dev, EKS ingress, and
    the production VM nginx proxy.
    """
    return send_from_directory(os.path.join(BASE_DIR, 'assets'), filename)


def register_static_routes(app):
    """Registers static routes Blueprint with the Flask app."""
    app.register_blueprint(static_bp)
