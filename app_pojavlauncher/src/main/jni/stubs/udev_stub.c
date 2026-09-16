#include <stddef.h>
#include <sys/types.h>

struct udev {};
struct udev_device {};
struct udev_enumerate {};
struct udev_list_entry {};

struct udev *udev_new(void) { return NULL; }
struct udev *udev_ref(struct udev *udev) { (void)udev; return NULL; }
struct udev *udev_unref(struct udev *udev) { (void)udev; return NULL; }

struct udev_enumerate *udev_enumerate_new(struct udev *udev) { (void)udev; return NULL; }
struct udev_enumerate *udev_enumerate_ref(struct udev_enumerate *udev_enumerate) { (void)udev_enumerate; return NULL; }
struct udev_enumerate *udev_enumerate_unref(struct udev_enumerate *udev_enumerate) { (void)udev_enumerate; return NULL; }

int udev_enumerate_add_match_subsystem(struct udev_enumerate *udev_enumerate, const char *subsystem) { (void)udev_enumerate; (void)subsystem; return -1; }
int udev_enumerate_add_match_sysattr(struct udev_enumerate *udev_enumerate, const char *sysattr, const char *value) { (void)udev_enumerate; (void)sysattr; (void)value; return -1; }
int udev_enumerate_add_match_property(struct udev_enumerate *udev_enumerate, const char *property, const char *value) { (void)udev_enumerate; (void)property; (void)value; return -1; }
int udev_enumerate_add_match_sysname(struct udev_enumerate *udev_enumerate, const char *sysname) { (void)udev_enumerate; (void)sysname; return -1; }
int udev_enumerate_add_match_tag(struct udev_enumerate *udev_enumerate, const char *tag) { (void)udev_enumerate; (void)tag; return -1; }
int udev_enumerate_add_match_parent(struct udev_enumerate *udev_enumerate, struct udev_device *parent) { (void)udev_enumerate; (void)parent; return -1; }
int udev_enumerate_add_match_is_initialized(struct udev_enumerate *udev_enumerate) { (void)udev_enumerate; return -1; }
int udev_enumerate_add_nomatch_subsystem(struct udev_enumerate *udev_enumerate, const char *subsystem) { (void)udev_enumerate; (void)subsystem; return -1; }
int udev_enumerate_add_nomatch_sysattr(struct udev_enumerate *udev_enumerate, const char *sysattr, const char *value) { (void)udev_enumerate; (void)sysattr; (void)value; return -1; }
int udev_enumerate_scan_devices(struct udev_enumerate *udev_enumerate) { (void)udev_enumerate; return -1; }
int udev_enumerate_scan_subsystems(struct udev_enumerate *udev_enumerate) { (void)udev_enumerate; return -1; }

struct udev_list_entry *udev_enumerate_get_list_entry(struct udev_enumerate *udev_enumerate) { (void)udev_enumerate; return NULL; }
struct udev_list_entry *udev_list_entry_get_next(struct udev_list_entry *list_entry) { (void)list_entry; return NULL; }
struct udev_list_entry *udev_list_entry_get_by_name(struct udev_list_entry *list_entry, const char *name) { (void)list_entry; (void)name; return NULL; }
const char *udev_list_entry_get_name(struct udev_list_entry *list_entry) { (void)list_entry; return NULL; }
const char *udev_list_entry_get_value(struct udev_list_entry *list_entry) { (void)list_entry; return NULL; }

struct udev_device *udev_device_new_from_syspath(struct udev *udev, const char *syspath) { (void)udev; (void)syspath; return NULL; }
struct udev_device *udev_device_new_from_devnum(struct udev *udev, char type, dev_t devnum) { (void)udev; (void)type; (void)devnum; return NULL; }
struct udev_device *udev_device_new_from_subsystem_sysname(struct udev *udev, const char *subsystem, const char *sysname) { (void)udev; (void)subsystem; (void)sysname; return NULL; }
struct udev_device *udev_device_new_from_environment(struct udev *udev) { (void)udev; return NULL; }
struct udev_device *udev_device_ref(struct udev_device *udev_device) { (void)udev_device; return NULL; }
struct udev_device *udev_device_unref(struct udev_device *udev_device) { (void)udev_device; return NULL; }

const char *udev_device_get_syspath(struct udev_device *udev_device) { (void)udev_device; return NULL; }
const char *udev_device_get_sysname(struct udev_device *udev_device) { (void)udev_device; return NULL; }
const char *udev_device_get_sysnum(struct udev_device *udev_device) { (void)udev_device; return NULL; }
const char *udev_device_get_devpath(struct udev_device *udev_device) { (void)udev_device; return NULL; }
const char *udev_device_get_devtype(struct udev_device *udev_device) { (void)udev_device; return NULL; }
const char *udev_device_get_subsystem(struct udev_device *udev_device) { (void)udev_device; return NULL; }
const char *udev_device_get_devnode(struct udev_device *udev_device) { (void)udev_device; return NULL; }
const char *udev_device_get_action(struct udev_device *udev_device) { (void)udev_device; return NULL; }
const char *udev_device_get_driver(struct udev_device *udev_device) { (void)udev_device; return NULL; }
const char *udev_device_get_property_value(struct udev_device *udev_device, const char *key) { (void)udev_device; (void)key; return NULL; }
const char *udev_device_get_sysattr_value(struct udev_device *udev_device, const char *sysattr) { (void)udev_device; (void)sysattr; return NULL; }
