///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

export interface Favicon {
  url?: string;
}

export interface Palette {
  type: string;
  extends?: string;
  colors?: { [hue: string]: string };
}

export interface PaletteSettings {
  primaryPalette?: Palette;
  accentPalette?: Palette;
}

export interface WhiteLabelingParams {
  logoImageUrl?: string;
  logoImageHeight?: number;
  appTitle?: string;
  favicon?: Favicon;
  paletteSettings?: PaletteSettings;
  helpLinkBaseUrl?: string;
  uiHelpBaseUrl?: string;
  enableHelpLinks?: boolean;
  whiteLabelingEnabled?: boolean;
  showNameVersion?: boolean;
  platformName?: string;
  platformVersion?: string;
  customCss?: string;
  hideConnectivityDialog?: boolean;
  overrideTrendzName?: boolean;
}

export interface LoginWhiteLabelingParams extends WhiteLabelingParams {
  pageBackgroundColor?: string;
  darkForeground?: boolean;
  domainName?: string;
  domainId?: { entityType: string; id: string };
  baseUrl?: string;
  prohibitDifferentUrl?: boolean;
  adminSettingsId?: string;
  showNameBottom?: boolean;
}

export const defaultWhiteLabelingParams: WhiteLabelingParams = {
  logoImageUrl: null,
  logoImageHeight: 36,
  appTitle: null,
  favicon: null,
  paletteSettings: {
    primaryPalette: { type: 'tb-primary' },
    accentPalette: { type: 'tb-accent' }
  },
  enableHelpLinks: true,
  whiteLabelingEnabled: true,
  showNameVersion: false,
  platformName: null,
  platformVersion: null,
  customCss: null
};

export const defaultLoginWhiteLabelingParams: LoginWhiteLabelingParams = {
  ...defaultWhiteLabelingParams,
  pageBackgroundColor: '#eee',
  darkForeground: false,
  showNameBottom: false
};

/**
 * Predefined Angular Material palette names selectable in the white-labeling form.
 * 'custom' allows specifying individual hue colors.
 */
export const materialColorPalettes: string[] = [
  'red', 'pink', 'purple', 'deep-purple', 'indigo', 'blue', 'light-blue', 'cyan',
  'teal', 'green', 'light-green', 'lime', 'yellow', 'amber', 'orange', 'deep-orange',
  'brown', 'grey', 'blue-grey'
];

// Hue spectrum shown in the palette editor dialog, in PE display order.
export const paletteHues: string[] =
  ['50', '100', '200', '300', '400', '500', '600', '700', '800', '900', 'A100', 'A200', 'A400', 'A700'];

// PE labels particular hues in the palette editor.
export const paletteHueLabels: {[hue: string]: string} = {
  300: 'white-labeling.hue1',
  500: 'white-labeling.primary-background',
  600: 'white-labeling.secondary-background',
  800: 'white-labeling.hue2',
  A100: 'white-labeling.hue3'
};
