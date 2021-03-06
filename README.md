# Note2JPG
![Java CI](https://github.com/Boomaa23/Note2JPG/workflows/Java%20CI/badge.svg)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/Boomaa23/Note2JPG)

```
    _   __      __      ___       ______  ______
   / | / /___  / /____ |__ \     / / __ \/ ____/
  /  |/ / __ \/ __/ _ \__/ /__  / / /_/ / / __  
 / /|  / /_/ / /_/  __/ __// /_/ / ____/ /_/ /  
/_/ |_/\____/\__/\___/____/\____/_/    \____/    
                                   
```

Converts Notability .note files into .jpg images

github.com/Boomaa23/Note2JPG | Copyright © 2020-2021. All Rights Reserved

## Quickstart
A comprehensive quickstart guide can be found [here](https://github.com/Boomaa23/Note2JPG/blob/master/quickstart.md) for a standard and easier setup.

## Installation
Download the [updater JAR file](https://github.com/Boomaa23/Note2JPG/blob/master/Note2JPGUpdater.jar?raw=true). Run the updater JAR once to get the application JAR and dependencies.

A copy of Java JDK 11 or newer must be present on the system at the time of installation.

## Usage
Double-click the application JAR to use the program once downloaded with dependencies. 

From the command line, this is `java -jar Note2JPG.jar`.

## Integration
### Google
Used to store note files automatically. Most users can use OAuth and login with a Google account if an applicable feature is enabled through the config JSON or parameters. 
Use `--grelog` to change accounts. Service accounts are also supported. 

### NEO
NEO integration requires that a NEO username and password is provided to the GUI each time (the user will be automatically prompted for this), 
passed through the parameter `--neo`, or included in the NEO keys in the config JSON.

## Parameters
The list of parameters to pass to the command is listed below. To store configs without passing them through each time, modify `config.json`.

| Key | Flag \<Value> | Source | JSON Type | Action
|-------------------------------|-----------------------|------------|---------|------------------------------------|
| Filename                      | -f \<filename>        | Note2JPG   | String  | Specify name of .note file
| ImageScaleFactor              | -s \<scaleFactor>     | Note2JPG   | int     | Multiplier to superscale the whole image by
| PDFScaleFactor                | -p \<pdfScale>        | Note2JPG   | int     | Multiplier to superscale the PDFs by
| Concatenate                   | --concat              | Note2JPG   | boolean | Concatenate note sources into one output
| ConvertAll                    | --all                 | Note2JPG   | boolean | Convert all available notes
| DisplayConverted              | --display             | Note2JPG   | boolean | Show the image after processing
| PageCountOut                  | --pgct \<numPages>    | Note2JPG   | int     | Force the number of output pages
| PageSelectionIn               | --pgsel \<pgSel>      | Note2JPG   | String  | Select note in pages (sep. by "/")
| NoPagePrompt                  | --npp                 | Note2JPG   | boolean | Disable the page prompt (all pages always)
| NoteFilter                    | --filter \<toMatch>   | Note2JPG   | String  | Specify a filter for note listing
| FitExactHeight                | --hfit                | Note2JPG   | boolean | Cut the image directly after writing/PDFs
| OutputDirectory               | --outdir \<path>      | Note2JPG   | String  | Specify an output directory
| NoFileOutput                  | --nofile              | Note2JPG   | boolean | Do not write the image to file
| RandomFile                    | --randomfile          | Note2JPG   | boolean | Select a file randomly if not specified
| GenerateConfig                | --genconfig           | Note2JPG   | boolean | Generate a config file template
| WriteConfig                   | --writeconfig         | Note2JPG   | boolean | Write out the current config to json
| ConsoleOnly                   | --console             | Note2JPG   | boolean | Use the system console instead of the GUI
| NEOUsername <br> NEOPassword  | --neo \<userId> \<pw> | NEO        | String  | Use NEO integration for unsubmitted assignments
| NEOClassID                    | --classid \<classid>  | NEO        | String  | Specify a class ID for NEO integration
| NEOAssignment                 | -a \<assignment>      | NEO        | String  | Set a different assignment name for image
| NEONoLink                     | --neonolink           | NEO        | boolean | Don't link image to NEO assignment
| AllowSubmitted                | --allowsubmitted      | NEO        | boolean | Allow submission of done assignments
| IncludeUnits                  | --inclunits           | NEO        | boolean | Include assignments from the units page
| NewNEOFilename                | --newneofn            | NEO        | boolean | Register a new NEO filename (no overwriting)
| WipeUploaded                  | --wipeup              | NEO/AWS    | boolean | Wipe uploaded images from specified sources
| UseAWS                        | --aws                 | Amazon     | boolean | Upload images to AWS (via NEO)
| UseDrive                      | --gdrive              | Google     | boolean | Use Google Drive to download and upload
| UseDriveDownload              | --gdrivedl            | Google     | boolean | Use Google Drive as a .note source
| UseDriveUpload                | --gdriveup            | Google     | boolean | Upload images to Google Drive
| ForceDriveDownload            | --fgdl                | Google     | boolean | Force the .note to download from Google
| GoogleSvcAcct                 | --gsvc                | Google     | boolean | Use a Google Service Account over OAuth
| GoogleRelog                   | --grelog              | Google     | boolean | Log in to a different Google account
| LimitDriveNotes               | --gdrivelim \<limit>  | Google     | int     | Define a limit for Drive-retrieved notes

Source code for all parameters can be found [here](https://github.com/Boomaa23/Note2JPG/blob/master/src/main/java/com/boomaa/note2jpg/config/Parameter.java)