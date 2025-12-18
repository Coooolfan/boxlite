"""
Centralized constants for BoxLite Python SDK.
"""

# Default VM resources
DEFAULT_CPUS = 2
DEFAULT_MEMORY_MIB = 512

# ComputerBox defaults (higher resources for desktop)
COMPUTERBOX_CPUS = 2
COMPUTERBOX_MEMORY_MIB = 2048
COMPUTERBOX_IMAGE = "lscr.io/linuxserver/webtop:ubuntu-xfce"

# ComputerBox display settings
DISPLAY_NUMBER = ":1"
DISPLAY_WIDTH = 1024
DISPLAY_HEIGHT = 768

# ComputerBox network ports (webtop defaults)
GUI_HTTP_PORT = 3000
GUI_HTTPS_PORT = 3001

# Timeouts (seconds)
DESKTOP_READY_TIMEOUT = 60
DESKTOP_READY_RETRY_DELAY = 0.5
