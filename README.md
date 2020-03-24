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



[com.boomaa.note2jpg.dependencies.Download .jar](https://github.com/Boomaa23/Note2JPG/blob/master/Note2JPG.jar?raw=true)

## Usage
`java -jar Note2JPG.jar`

| JSON Key | Flag \<Value> | Source | JSON Value | Action
|-------------------------------|-----------------------|----------|---------|------------------------------------|
| Filename                      |  -f \<filename>       | Note2JPG | String  | Specify name of .note file
| ImageScaleFactor              | -s \<scaleFactor>     | Note2JPG | int     | Multiplier to superscale the whole image by
| PDFScaleFactor                | -p \<pdfScaleFactor>  | Note2JPG | int     | Multiplier to superscale the PDFs by
| ConvertAll                    | --all                 | Note2JPG | boolean | Convert all available notes
| DisplayConverted              | --display             | Note2JPG | boolean | Show the image after processing
| NoFileOutput                  | --nofile              | Note2JPG | boolean | Do not write the image to file
| RandomFile                    | --randomfile          | Note2JPG | boolean | Select a file randomly if not specified
| NoTextBoxes                   | --notextboxes         | Note2JPG | boolean | Do not request positions for text boxes
| GenerateConfig                | --genconfig           | Note2JPG | boolean | Generate a config file template
| WriteConfig                   | --writeconfig         | Note2JPG | boolean | Write out the current config to json
| NEOUsername <br> NEOPassword  | --neo \<userId> \<pw> | NEO      | String  | Use NEO integration for unsubmitted assignments
| NEOClassID                    | --classid \<classid>  | NEO      | String  | Specify a class ID for NEO integration
| UseGoogleDrive                | --usedrive            | Google   | boolean | Use Google Drive as a .note source
| GoogleSvcAcctID               |  --gacctid \<acctid>  | Google   | String  | The Google service account ID to use

## Config JSON
If you want to store configs without passing them through each time, create a JSON called `config.json` in your root directory. Use keys as listed in the table above. If you would like a template generated, pass `--genconfig`. The application will stop after generating the config file.

## Integration
### Google
You will need to provide your own `.p12` private key and service account ID for Google integration. The private key should be renamed to `GoogleSvcAcctPrivateKey.p12`. The account ID can either be provided through `--gacctid` or through the config JSON.

### NEO
NEO integration requires that you set a NEO username and password each time through `--neo` or through the NEO keys in the config JSON. The default class ID can be overridden by `--classid` or through the config JSON.