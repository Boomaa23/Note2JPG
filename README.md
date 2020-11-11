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

github.com/Boomaa23/Note2JPG | Copyright © 2020. All Rights Reserved

## Quickstart
A comprehensive quickstart guide can be found [here](https://github.com/Boomaa23/Note2JPG/blob/master/quickstart.md) for a standard and easier setup.

## Installation
Download the updater JAR file listed below. Run the updater JAR once to get the application JAR and dependencies, then run the application JAR as listed in "Usage" each time you'd like to use the program.

Download: [Updater JAR](https://github.com/Boomaa23/Note2JPG/blob/master/Note2JPGUpdater.jar?raw=true)

## Usage
`java -jar Note2JPG.jar`

| JSON Key | Flag \<Value> | Source | JSON Value | Action
|-------------------------------|-----------------------|------------|---------|------------------------------------|
| Filename                      | -f \<filename>        | Note2JPG   | String  | Specify name of .note file
| ImageScaleFactor              | -s \<scaleFactor>     | Note2JPG   | int     | Multiplier to superscale the whole image by
| PDFScaleFactor                | -p \<pdfScale>        | Note2JPG   | int     | Multiplier to superscale the PDFs by
| ConvertAll                    | --all                 | Note2JPG   | boolean | Convert all available notes
| DisplayConverted              | --display             | Note2JPG   | boolean | Show the image after processing
| PageCount                     | --pg                  | Note2JPG   | int     | Force the number of output pages
| NoteFilter                    | --filter \<toMatch>   | Note2JPG   | String  | Specify a filter for note listing
| FitExactHeight                | --hfit                | Note2JPG   | boolean | Cut the image directly after writing/PDFs
| OutputDirectory               | --outdir \<path>      | Note2JPG   | String  | Specify an output directory
| NoFileOutput                  | --nofile              | Note2JPG   | boolean | Do not write the image to file
| RandomFile                    | --randomfile          | Note2JPG   | boolean | Select a file randomly if not specified
| NoTextBoxes                   | --notextboxes         | Note2JPG   | boolean | Do not request positions for text boxes
| NoEmbedImages                 | --noembedimages       | Note2JPG   | boolean | Do not request positions for images in notes
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
| GoogleSvcAcct                 | --gsvc                | Google     | boolean | Use a Google Service Account over OAuth
| LimitDriveNotes               | --gdrivelim \<limit>  | Google     | int     | Define a limit for Drive-retrieved notes

More information on all parameters can be found [here](https://github.com/Boomaa23/Note2JPG/blob/master/src/main/java/com/boomaa/note2jpg/config/Parameter.java)

## Config JSON
If you want to store configs without passing them through each time, modify the `config.json` file in your root directory. Use keys as listed in the table above. 

## Integration
### Google
Most users can use the per-login Google OAuth. However, if don't want to login every time you will need to provide your own `.json` private key and service account ID.
The private key should be renamed to `GoogleSvcAcctPrivateKey.json`.

### NEO
NEO integration requires that you set a NEO username and password each time through `--neo` or through the NEO keys in the config JSON.