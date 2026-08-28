# The display HAT EEPROM and the camera share the VideoCore I2C bus on Pi Zero.
# Firmware before 2025-08-20 probes the HAT EEPROM even when
# force_eeprom_read=0, which can leave the IMX708 inaccessible. Pin the first
# firmware release containing the fix so the display can use the full header.
RPIFW_DATE = "20250820"
SRCREV = "511dd35ba1fa3e263b3a142e489200ea8d513fed"
SRC_URI[sha256sum] = "230fa688d2f4f29adca2cdc3fac646f4330bd0575ab36b8e13ec00d5218977a3"
