name=BTWizardAPITestApp
foldername=BTWizardAPITestApp
android update project -p . -n $name -t android-10
ant clean
ant debug
