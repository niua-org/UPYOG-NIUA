FROM python:3.12-slim
# Install system dependencies needed for compiling python packages (e.g. faiss)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
# Copy and install python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
# Pre-download SentenceTransformer models during build time
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-mpnet-base-v2')"
# Copy source code and files (including UpyogFAQ.csv and index.html)
COPY . .
EXPOSE 8090
CMD ["python", "app.py"]