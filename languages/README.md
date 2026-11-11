# Language packs (development build)

The app includes English (`en.json`) and Korean (`ko.json`) in
`app/src/main/assets/languages/`. In Settings, choose Language to cycle through
installed packs, or choose Import language pack and select a JSON file in the
Android document picker. Import is available only on a head unit with a
compatible document picker. Packs are data, not executable code, and are copied
to app-private storage; removing the app removes imported packs.

To contribute a language, copy `en.json`, set a unique BCP 47 `locale` and a
native `name`, and translate the values under `strings`. Keep keys, format 1,
and placeholders such as `%1$s` unchanged. Missing entries fall back to English.
Use a new locale; the built-in English and Korean packs cannot be replaced.
Files are limited to 128 KiB, must contain only known keys and matching
placeholders, and cannot overwrite an existing imported locale. Validate with
`python3 tools/check_language_pack.py languages/en.json your-pack.json`.

Some historical logs and unknown system error details are retained as originally
recorded, so they may remain in their original language. English-to-Korean
switching and the main settings screen were checked on one vehicle. Importing a
pack was not verified there: the document picker did not appear. Other screens
and head units need further validation.
