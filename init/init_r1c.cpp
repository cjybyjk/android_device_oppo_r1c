/*
   Copyright (c) 2016, The CyanogenMod Project
             (c) 2017, The LineageOS Project

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
   ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
   OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
   IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <cstdlib>
#include <unistd.h>
#include <fcntl.h>
#define _REALLY_INCLUDE_SYS__SYSTEM_PROPERTIES_H_
#include <sys/_system_properties.h>

#include "vendor_init.h"
#include "property_service.h"
#include "log.h"
#include "util.h"

void property_override(char const prop[], char const value[])
{
    prop_info *pi;

    pi = (prop_info*) __system_property_find(prop);
    if (pi)
        __system_property_update(pi, value, strlen(value));
    else
        __system_property_add(prop, strlen(prop), value, strlen(value));
}

static int read_file2(const char *fname, char *data, int max_size)
{
    int fd, rc;

    if (max_size < 1)
        return 0;

    fd = open(fname, O_RDONLY);
    if (fd < 0) {
        ERROR("failed to open '%s'\n", fname);
        return 0;
    }

    rc = read(fd, data, max_size - 1);
    if ((rc > 0) && (rc < max_size))
        data[rc] = '\0';
    else
        data[0] = '\0';
    close(fd);

    return 1;
}

void init_alarm_boot_properties()
{
    char const *alarm_file = "/proc/sys/kernel/boot_reason";
    char buf[64];

    if(read_file2(alarm_file, buf, sizeof(buf))) {

    /*
     * Setup ro.alarm_boot value to true when it is RTC triggered boot up
     * For existing PMIC chips, the following mapping applies
     * for the value of boot_reason:
     *
     * 0 -> unknown
     * 1 -> hard reset
     * 2 -> sudden momentary power loss (SMPL)
     * 3 -> real time clock (RTC)
     * 4 -> DC charger inserted
     * 5 -> USB charger insertd
     * 6 -> PON1 pin toggled (for secondary PMICs)
     * 7 -> CBLPWR_N pin toggled (for external power supply)
     * 8 -> KPDPWR_N pin toggled (power key pressed)
     */
        if(buf[0] == '3')
            property_set("ro.alarm_boot", "true");
        else
            property_set("ro.alarm_boot", "false");
    }
}

void vendor_load_properties()
{
    char const *prj_file = "/proc/oppoVersion/prjVersion";
    char prj_version[64];

    if (read_file2(prj_file, prj_version, sizeof(prj_version))) {
        if (strstr(prj_version, "14045")) {
            property_override("ro.product.model", "R8207");
            property_set("ro.common.soft", "MSM_14045");
            property_override("ro.product.name", "R8207");
            property_override("ro.build.product", "R8207");
            property_override("ro.build.fingerprint", "OPPO/R8207/R1C:4.4.4/KTU84P/1390465867:user/release-keys");
            property_override("ro.build.description", "msm8916_32-user 4.4.4 KTU84P eng.root.20151213 release-keys");
        } else if (strstr(prj_version, "14046")) {
            property_override("ro.product.model", "R8200");
            property_set("ro.common.soft", "MSM_14046");
            property_override("ro.product.name", "R8200");
            property_override("ro.build.product", "R8200");
            property_override("ro.build.fingerprint", "OPPO/R8200/R1C:4.4.4/KTU84P/1390465867:user/release-keys");
            property_override("ro.build.description", "msm8916_32-user 4.4.4 KTU84P eng.root.20150515 release-keys");
        } else if (strstr(prj_version, "14047")) {
            property_override("ro.product.model", "R8205");
            property_set("ro.common.soft", "MSM_14047");
            property_override("ro.product.name", "R8205");
            property_override("ro.build.product", "R8205");
            property_override("ro.build.fingerprint", "OPPO/R8205/R1C:4.4.4/KTU84P/1390465867:user/release-keys");
            property_override("ro.build.description", "msm8916_32-user 4.4.4 KTU84P eng.root.20151215 release-keys");
        }
    }

    init_alarm_boot_properties();
}