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



[Download .jar](https://github.com/Boomaa23/Note2JPG/blob/master/Note2JPG.jar?raw=true)

## Usage
`java -jar Note2JPG.jar`

| Flag \<Value>         | Action |
|-----------------------|--------|
| -f \<filename>        | Specify name of .note file
| -s \<scaleFactor>     | Multiplier to superscale the whole image by [1-32]
| -p \<pdfScaleFactor>  | Multiplier to superscale the PDFs by [1-5]
| --neo \<userId> \<pw> | Use NEO integration for unsubmitted assignments
| --classid \<classid>  | Specify a class ID for NEO integration
| --usedrive            | Use Google Drive as a .note source
| --all                 | Convert all available notes
| --display             | Show the image after processing
| --nofile              | Do not write the image to file
| --randomfile          | Select a file randomly if not specified
| --notextboxes         | Do not request positions for text boxes

## Google Integration
You will need to provide your own `.p12` credentials file for Google Drive and account ID. Rename the private key to `GoogleSvcAcctPrivateKey.p12` and put the Service Account ID in a file called `GoogleSvcAcctID.conf`. Both should be in the root directory of the jar or project.
