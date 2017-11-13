# LinearLayoutExpandableandMovable
An example android project for expandable linear layout and can be movable anywhere in the screen.

Here you can check how it works:

![Alt Text](https://github.com/adarshvris/LinearLayoutExpandableandMovable/blob/master/video_gif.gif)


# Working
In this project, you can expect movable layout any where on the screen(i.e layouts will be collapsed).
On click of the icon the layout will get expand and you can see the items.

For this app we require the following permission.

<b>uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"</b>

Note: Till kitkat version, the permission will be internally granted but from lollipop(5.0 and above) manually you have to grant the permission.

<b>From lollipop onwards you have to add the following code to grant the permission:</b>

if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.L)
        {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            }
        }
        
This will redirect you to <b>"system/draw over other apps"</b>

# License

Copyright 2017 Adarsh

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
