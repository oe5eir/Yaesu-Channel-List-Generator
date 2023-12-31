package at.oe5eir.yaesu.channels.config;

/*
 *  Copyright (C) 2023 OE5EIR @ https://www.oe5eir.at/
 *
 *  Licensed under the GNU General Public License v3.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *
 *  You may obtain a copy of the License at
 *      https://www.gnu.org/licenses/gpl-3.0.en.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

public enum OffsetDirection {
    OFF("OFF"),
    RPTP("+RPT"),
    RPTM("-RPT"),
    PM("-/+");

    private final String identifier;

    OffsetDirection(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return identifier;
    }
}
