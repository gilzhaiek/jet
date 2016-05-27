To create the SDK:

In this folder, update the revision in manifest.ini

from mydroid run:
./build_sdk_jet.sh
This will create a zip file in out/host/linux-x86/sdk_addon/jet_sdk_addon-eng.XXXX.zip

Fix the docs folder:
Unzip the file and rename "com.reconinstruments.os_docs" in the docs folder to reference
This will fix the problem where javadocs are not present in Eclipse
Zip the result and that will be your new zip file.

--------- repository.xml ---------
To get the sh1 value:
sha1sum <zipFile>

TO get the size:
ls -l <zipFile>

Create a file called repository.xml from this content, while updating revision to the same value you put in the manifest.ini
desc-url should contain the folder where the zip file is located

Upload both repository.xml and the zip file to the desc-url folder
-------------------
<?xml version="1.0" encoding="UTF-8"?>

<sdk:sdk-addon
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:sdk="http://schemas.android.com/sdk/android/addon/1">

    <sdk:add-on>
        <sdk:name>Recon Instruments SDK Add-On</sdk:name>
        <sdk:api-level>16</sdk:api-level>
        <sdk:vendor>Recon Instruments</sdk:vendor>
        <sdk:revision>2</sdk:revision>
        <sdk:description>Android + Recon Instruments SDK Add-on, API 16, revision 2 </sdk:description>
        <sdk:desc-url>https\://dl.dropboxusercontent.com/u/33672743/sdk-addon/</sdk:desc-url>
        <sdk:uses-license ref="recon-instruments-android-addon-license" />
        <sdk:archives>
            <sdk:archive os="any">
                <sdk:size>35482086</sdk:size>
                <sdk:checksum type="sha1">40ac34ec9846d931f914380d7ba86e052494e42f</sdk:checksum>
                <sdk:url>jet_sdk_addon-eng.gil-linux-x86.zip</sdk:url>
            </sdk:archive>
        </sdk:archives>
        <sdk:libs>
        </sdk:libs>
    </sdk:add-on>

    <sdk:license type="text" id="recon-instruments-android-addon-license">
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this SDK addon except in compliance with the License.
You may obtain a copy of the License at: http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
    </sdk:license>
</sdk:sdk-addon>
-------------------
