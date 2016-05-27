echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
echo 0 > /debug/pm_debug/sleep_while_idle
echo 0 > /debug/pm_debug/enable_off_mode
echo WAKE_LOCK_SUSPEND > /sys/power/wake_lock

