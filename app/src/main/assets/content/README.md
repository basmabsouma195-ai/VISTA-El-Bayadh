# VISTA offline content package

This directory is the bundled content root for the El Bayadh offline edition.

Content is stored locally in the installed application. The initial release must not
depend on a network request to display its core encyclopedia. Future content updates
are versioned separately from application updates and are copied into the local store
only after validation.

## Record requirements

Every published landmark record must include:
- Stable identifier and category identifier.
- Arabic, French and English names and descriptions.
- Editorial status: documented, oral account, or under investigation.
- Verified geographic coordinates before map publication.
- Locally bundled image references.
- Optional locally bundled audio references for each language.

Never fabricate historical claims, coordinates, opening hours, visitor guidance,
or safety information. Empty content is preferable to invented content.
