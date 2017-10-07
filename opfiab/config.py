def can_build(plat):
	return plat=="android"

def configure(env):
	if (env['platform'] == 'android'):
		env.android_add_java_dir("android/src")
		env.android_add_maven_repository("url 'https://raw.githubusercontent.com/onepf/OPF-mvn-repo/master/'")
		env.android_add_to_manifest("android/AndroidManifestChunk.xml")
		env.android_add_to_permissions("android/AndroidManifestPermissionsChunk.xml")
		env.android_add_dependency("compile 'de.greenrobot:eventbus:2.4.0'")
		env.android_add_dependency("compile 'org.onepf:opfutils:0.1.26'")
		env.android_add_dependency("compile 'org.onepf:opfiab:0.4.0@aar'")
		env.android_add_dependency("compile 'org.onepf:opfiab-amazon:0.4.0@aar'")
		env.android_add_dependency("compile 'org.onepf:opfiab-google:0.4.0@aar'")
		env.android_add_dependency("compile 'org.onepf:opfiab-openstore:0.4.0@aar'")
		env.android_add_dependency("compile 'org.onepf:opfiab-samsung:0.4.0@aar'")
		env.android_add_dependency("compile 'com.amazon:in-app-purchasing:+'")
