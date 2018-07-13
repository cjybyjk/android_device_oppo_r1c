#
# Copyright (C) 2015 The CyanogenMod Project
# Copyright (C) 2018 The LineageOS Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FORCE_32_BIT := true

LOCAL_PATH := device/oppo/r1c

# Overlay
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay

# Inherit from msm8939-common
$(call inherit-product, device/oppo/msm8939-common/msm8939.mk)

# Audio configuration
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/audio/audio_platform_info.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio_platform_info.xml \
    $(LOCAL_PATH)/audio/mixer_paths_mtp.xml:$(TARGET_COPY_OUT_VENDOR)/etc/mixer_paths_mtp.xml

# ACDB Loader calibration
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/audio/acdbdata/Bluetooth_cal.acdb:system/etc/acdbdata/MTP/Bluetooth_cal.acdb \
    $(LOCAL_PATH)/audio/acdbdata/General_cal.acdb:system/etc/acdbdata/MTP/General_cal.acdb \
    $(LOCAL_PATH)/audio/acdbdata/Global_cal.acdb:system/etc/acdbdata/MTP/Global_cal.acdb \
    $(LOCAL_PATH)/audio/acdbdata/Handset_cal.acdb:system/etc/acdbdata/MTP/Handset_cal.acdb \
    $(LOCAL_PATH)/audio/acdbdata/Hdmi_cal.acdb:system/etc/acdbdata/MTP/Hdmi_cal.acdb \
    $(LOCAL_PATH)/audio/acdbdata/Headset_cal.acdb:system/etc/acdbdata/MTP/Headset_cal.acdb \
    $(LOCAL_PATH)/audio/acdbdata/Speaker_cal.acdb:system/etc/acdbdata/MTP/Speaker_cal.acdb \
    $(LOCAL_PATH)/audio/acdbdata/WorkspaceFile.qwsp:system/etc/acdbdata/MTP/WorkspaceFile.qwsp

# Boot animation
TARGET_SCREEN_HEIGHT := 1280
TARGET_SCREEN_WIDTH := 720

# Camera
PRODUCT_PACKAGES += \
    libshim_camera \
    libboringssl-compat

# Dalvik
$(call inherit-product, frameworks/native/build/phone-xxhdpi-2048-dalvik-heap.mk)

# Density
PRODUCT_AAPT_CONFIG := normal
PRODUCT_AAPT_PREF_CONFIG := xhdpi

PRODUCT_PROPERTY_OVERRIDES += \
    ro.sf.lcd_density=320

# Input configuration
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/keylayout/gpio-keys.kl:$(TARGET_COPY_OUT_VENDOR)/usr/keylayout/gpio-keys.kl \
    $(LOCAL_PATH)/keylayout/synaptics-s3508-kpd.kl:$(TARGET_COPY_OUT_VENDOR)/usr/keylayout/synaptics-s3508-kpd.kl

# Ramdisk
PRODUCT_PACKAGES += \
    fstab.qcom \
    init.qcom.rc

# RIL
PRODUCT_PROPERTY_OVERRIDES += \
    persist.data.target=dpm1 \
    rild.libpath=/vendor/lib/libril-qc-qmi-1.so \
    ro.telephony.default_network=9,9 \
    ro.telephony.ril.config=simactivation,sim2gsmonly \
    persist.radio.multisim.config=dsds

# Wifi
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/wifi/WCNSS_qcom_wlan_nv.bin:$(TARGET_COPY_OUT_VENDOR)/firmware/wlan/prima/WCNSS_qcom_wlan_nv.bin

# Call the proprietary setup
$(call inherit-product, vendor/oppo/r1c/r1c-vendor.mk)
