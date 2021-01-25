# Note2JPG Quickstart Guide
This guide is intended for the user to be able to install and use Note2JPG as soon as possible. 
It is a basic installation and includes suggested features.

## Setup
### Application
This is the basic setup and is required to get the application working in any fashion on a computer.
1. Install Java JDK 11 or newer [here](https://www.oracle.com/java/technologies/javase-downloads.html).
2. Download the updater [here](https://github.com/Boomaa23/Note2JPG/blob/master/Note2JPGUpdater.jar?raw=true) and place in the folder you intend to run the application out of.
3. Run the updater by double-clicking it. Close the window by clicking "Finish" after configuring the options.

### Config
This allows you to set custom configuration options per the parameters [here](https://github.com/Boomaa23/Note2JPG#usage). 
The below listed options are important and should be set for a working quickstart application.
- NEOUsername: Set to your NEO username so assignments can be auto-uploaded.
- NEOPassword: Same as above. Set to your NEO password. Obviously don't share your configuration file.
- UseAWS = "true": Allows images to be uploaded to NEO automatically.
- UseDriveDownload = "true": Allows .note files to be downloaded from Google Drive automatically.

The following are optional but recommended.
- ImageScaleFactor = "12": Set to something between 6 and 16, setting to below 4 will cause images to appear blurry.
- PDFScaleFactor = "3": Same as above, image will be blurry with 1. Diminishing returns after 4. Be careful with PDFs over 10 pages.

Note that these options take exponentially more RAM. If your computer isn't great keep these settings low otherwise issues will occur.

To change these options, either modify their values during the installer or modify the `config.json` file in that was downloaded with the updater. 
It is a standard JSON format, and can be modified by changing the text in between the quotation marks before the commas.

### Notability
These changes are applied to the Notability app so that it exports files in the correct format to the correct place.
1. Open the Notability app
2. Click the gear (settings) in the bottom left
3. Tap "Auto-Backup" in the left menubar
4. Select "Google Drive" on the right hand side
5. Sign in to your school Google account. The school gives you unlimited storage whereas a personal account is 15GB limited.
6. (optional) Change the destination to wherever you want. It doesn't matter.

## Usage
If everything is set up correctly, the application should work after just double-clicking `Note2JPG.jar`.

Do not type into the GUI that appears unless you are prompted. Answer with the numbers only, not names associated with them. Prompts should be:
- Which class are you uploading to?
- What Google Drive file do you want to parse?
- What assignment in the class should this be uploaded to?

If everything goes correctly, it should display the location of the uploaded image and then the submitted NEO assignment.

To resubmit something again, change `AllowSubmitted` to `"true"` in the config.

If there are any issues to report, please write a GitHub issue about it [here](https://github.com/Boomaa23/Note2JPG/issues). This is still a work in progress, and may have some bugs.

### Google Service Account (Optional)
If you used Google to automatically backup your notes, you will be prompted to accept a Google OAuth request every time you open the program.
However, if you set up a Google Service Account it will automatically log you in. This is optional but suggested if you plan to use the program often.

This allows notes to automatically be read by Note2JPG and should be done on the same computer you're installing the software onto. 
If you used a school account, this must be done on another account because of district restrictions. 
1. Go to https://console.developers.google.com/ in a web browser
2. Create a new project. A popup may appear automatically or you may have to click at the top on the hexagons.
3. Go to the library tab on the left.
4. Search for or find the "Google Drive API" tile and click on it.
5. Click the "Enable" button
6. Go back to the home using the on-page back button (not browser)
7. Click on "Credentials" on the left
8. Click on "Manage Service Accounts"
9. Click "Create Service Account" at the top
10. Name the service account whatever you want with whatever description. It doesn't matter.
11. Once the service account has been created, click on it.
12. Click "Add Key" then "Create New Key". Stay with JSON then click "Create".
13. Rename the downloaded file to `GoogleSvcAcctPrivateKey.json` and put it in the same folder as the application.

Do not share the `GoogleSvcAcctPrivateKey.json` key file with anyone or put it in a publicly accessible place. It provides access to your Google account.

If you used a school account for the backup, share the Notability backup folder in Google Drive with the other personal account that has the service account tied to it.
Ensure the account has editing access. If you used a personal account for both, disregard this.