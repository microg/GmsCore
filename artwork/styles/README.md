## The files

* `style-microg-normal-openmaptiles.json` is a pure OpenMapTiles schema version of microG's custom style. The sources are set to Stadia but could be modified to be used with another schema-compliant vendor, e.g. MapTiler.
* `style-microg-normal-stadia.json` is a variant with Stadia custom layers.
* `style-microg-normal-mapbox.json` is a historic, unmaintained variant for use with Mapbox.
* `style-microg-sattelite-*.json` is the same, but with a sattelite layer (no openmaptiles variant, because openmaptiles does not specify a sattelite layer). **TODO: create these.**

* `sprite_sources` files can be generated to a single sprite file as found in the app's assets using `spreet` (see below).
* **TODO: remove references to mapbox for fonts.**

## Resources

### For creating styles

* Tool: https://maputnik.github.io/
* Style spec: https://maplibre.org/maplibre-style-spec/
	* https://maplibre.org/maplibre-style-spec/expressions/
* Style schema: **https://openmaptiles.org/schema/**
* Schema tileset explorer (requires maptiler login): https://cloud.maptiler.com/tiles/v3-openmaptiles/

#### Vendor-specific
* Mapbox
    * https://docs.mapbox.com/data/tilesets/reference/mapbox-streets-v8/
* Stadia
    * https://docs.stadiamaps.com/custom-styles/
    * https://docs.stadiamaps.com/tilesets/
    
### For converting assets

* https://github.com/flother/spreet
* https://github.com/stadiamaps/sdf_font_tools/tree/main/build_pbf_glyphs

Commands:

```
$ spreet sprite_sources/ ../../play-services-maps-core-mapbox/src/main/assets/sprites
$ spreet --retina sprite_sources/ ../../play-services-maps-core-mapbox/src/main/assets/sprites@2x
$ build_pbf_glyphs fonts/ ../../play-services-maps-core-mapbox/src/main/assets/
```

## Legal

* based on Mapbox Basic, part of the Mapbox Open Styles, licensed under
    * Style code: BSD license
    * Style virtual features / design: CC BY 3.0
    * Reference: https://github.com/mapbox/mapbox-gl-styles/blob/master/LICENSE.md
* derivation created by larma
* derivation created by /e/ foundation
* makes use of snippets from [Stadia's variant of OSM Bright](https://docs.stadiamaps.com/map-styles/osm-bright/), also a derivation of Mapbox Open Styles
	* Style code: BSD 3-Clause License
	* Style design: CC-BY 4.0
	* Reference: https://stadiamaps.com/attribution/
* fonts: Roboto family, licensed Apache 2.0, https://fonts.google.com/specimen/Roboto/about
