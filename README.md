# Channel List Generator for Yaesu FT3D Radios

This application creates the repeater list based on the OEVSV database for Yaesu FT3D radios which can be imported to the radio via the CPS.
It generates a channel list with all Austrian FM and C4FM repeaters.

## Requirements

 - Java SE 8 Runtime Environment or newer
 - Yaesu FT3D Programmer ADMS-11 Version 1.0.0.0 or newer
 - IntelliJ IDEA (if you want to modify this program)

## Run the application
Open a shell on your machine and navigate to the folder containing the downloaded or self-created JAR file.

Then run: `java -jar yaesu-channel-list-generator.jar [OUTPUT FILE]`

## Import into CPS

 1. Read the radio to the microSD card (`DISP > SD CARD > 1 BACKUP > 1 Write to SD > OK`)
 2. Import codeplug (BACKUP.dat) from the SD card to CPS (`Communications > Get data from SD card...`).
 3. Import repeater list to codeplug (`File > Import`)
 4. Select desired Priority CH and Banks
 5. Export codeplug (BACKUP.dat) from the CPS to the SD card (`Communications > Send data to SD card...`).
 6. Write the radio (`DISP > SD CARD > 1 BACKUP > 2 Read from SD > OK`) and wait until the radio rebooted.

## Restrictions

The FT3D can't have different CTCSS settings for Rx and Tx. The Rx CTCSS setting of the repeater (Tx on the radio) is used for Rx and Tx.
If Rx and Tx CTCSS settings don't match, a warning is displayed.

## License

Copyright (C) 2023 OE5EIR @ https://www.oe5eir.at/

Licensed under the GNU General Public License v3.0 (the "License"). \
You may not use this project except in compliance with the License.

You may obtain a copy of the License at: \
https://www.gnu.org/licenses/gpl-3.0.en.html

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
