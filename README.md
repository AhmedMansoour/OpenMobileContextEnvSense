# OpenMobileContextEnvSense

OpenMobileContextEnvSense is an open-source Android-based sensing framework and dataset ecosystem for multimodal mobile environmental understanding, covering indoor–outdoor inference, transition-zone ambiguity, and deployment-oriented evaluation under device heterogeneity.

**Paper (under review):** *Understanding Unseen Environments with Multimodal Mobile Sensing: A Review of Computational Methods and Zero-Shot Generalization Challenges*

**Authors:** Ahmed Mansour, Wu Chen, Jingxian Wang, Mahmoud Adham, Mohamed Elrifaee, Mostafa Mahmoud

## What is included in this upload pack
- A **public sample dataset** (one session) under `data/datasets/`
- A **dataset description** and schema notes
- **Plots** and **app UI screenshots** under `assets/`
- Your analysis notebook placeholder under `notebooks/` (copied from the provided file)
- Minimal scripts for validation and session summarization

## Quickstart (local)
```bash
python -m venv .venv
# Windows: .venv\Scripts\activate
pip install -r requirements.txt

python scripts/summarize_session.py --zip data/datasets/IOD_Record_1761144837784_public.zip
```

## Dataset pack
See `data/datasets/README.md`.
