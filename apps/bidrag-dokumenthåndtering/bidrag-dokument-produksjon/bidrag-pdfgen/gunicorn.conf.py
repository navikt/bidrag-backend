"""gunicorn hooks for prometheus_client multiprocess mode.

The worker settings stay on the command line in DockerfileWeasyprint. This file
only keeps the shared metrics directory consistent: it is emptied when the
master starts, and a worker's live gauge files are removed when it exits
(--max-requests recycles workers regularly).

Do not import metrics.py here: it would create metric files from the master
process before on_starting has created the directory.
"""

import os
import shutil

from prometheus_client import multiprocess

# The variable prometheus_client itself reads to enable multiprocess mode.
MULTIPROC_DIR_ENV = "PROMETHEUS_MULTIPROC_DIR"


def on_starting(_server):
    directory = os.environ.get(MULTIPROC_DIR_ENV)
    if not directory:
        return
    shutil.rmtree(directory, ignore_errors=True)
    os.makedirs(directory, exist_ok=True)


def child_exit(_server, worker):
    if os.environ.get(MULTIPROC_DIR_ENV):
        multiprocess.mark_process_dead(worker.pid)
