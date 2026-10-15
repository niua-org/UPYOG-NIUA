# Trade License — how the citizen new application works

This note explains the citizen **New Trade License** screen we built. It is written so you can follow the flow without reading every file first.

The form is one page with three panels. The citizen fills the open panel. The next panel opens only after the required fields in the current one are filled. After that they review the answers and submit.

To add or change a question, edit `AccordionNewApplication.json`. Short editing notes are in `TL_README.md`.

## What the citizen sees

The route is the citizen new-application path. Four screens run in this order:

| Screen | Route | What it shows |
| --- | --- | --- |
| Documents | `/info` | Proofs to keep ready (identity, ownership, photo) |
| Application | `/apply` | The accordion form |
| Review | `/check` | Answers grouped for a final look, plus a declaration |
| Result | `/acknowledgement` | Application number after the server accepts it |

A progress bar sits on the application and review screens. It has four steps: Trade Details, Location & Owner Details, Document Details, and Summary. The documents screen and the result screen hide the bar.

## The three panels

All three sit on `/apply`. `confirm` in the JSON is the review page, not a fourth panel.

**1. Trade Details** (`structure`)

- Is the trade in a building, or does it move (a cart or vehicle)?
- Building type, or vehicle type.
- For a building: does the citizen already have a property? Yes opens a search. No sends them to create a property first.
- For a moving trade, the property questions stay hidden.

**2. Location & trade** (`tradeLocation`)

- Trade name and commencement date (must fall in the current financial year).
- One or more trade units: category, type, subtype, unit of measure, and the quantity.
- Optional accessories (sign board, and so on), with their own unit and quantity.
- GST, operational area, number of employees.
- Address: pincode, city, locality, street, door number, landmark. A linked property fills this in.

**3. Ownership & documents** (`ownersDocs`)

- Can the owners be copied from the property?
- Ownership category and one or more owners (name, gender, mobile, father or husband, relationship, email, address).
- Uploads: proof of identity, proof of ownership, owner photo.

## How the pieces fit

The screen is config-driven. The JSON decides the questions. A small set of JavaScript files does the work JSON cannot do: search, price slabs, copying an address, and turning the answers into the payload the old check page already understands.

```
AccordionNewApplication.json     questions, panels, show/hide, timeline
        +
tlFormConfig.js                  rules JSON should not own
        |
        v
Create/index.js                  loads the config and the four routes
        |
        v
TLCitizenNewApplication.js       the accordion itself
        |
        +-- tlPropertySearch.js   property ID or mobile lookup
        +-- tlBillingSlab.js      unit of measure and allowed range
        |
        v
tlSessionAdapter.js              flat answers → shape the review page expects
        |
        v
Check page  →  Acknowledgement   existing submit (convertToTrade)
```

On load, `Create/index.js` reads the local JSON. If MDMS `TradeLicense.NewApplication` is present, that master can replace it. The local file is kept so the screen still works when MDMS is old or missing. Local rules in `tlFormConfig.js` are merged on top, so a stale master cannot drop the property search or the financial-year check.

Answers are stored in the browser under the session key `TL_CREATE_TRADE`, step `newApplication`. Refreshing the page keeps the draft.

## What lives in JSON, and what lives in code

**JSON** (`AccordionNewApplication.json`)

- Panel order and titles
- Each question: label, type, required or optional
- `visibleWhen` — show a question only when another answer matches
- Timeline labels
- Document-page headings (`navigation.infoPage`)

**Code**

| File | Job |
| --- | --- |
| `pages/citizen/Create/index.js` | Wires the four routes and picks the JSON |
| `pageComponents/TLCitizenNewApplication.js` | Accordion. Connects search, slabs, and address copy |
| `config/tlFormConfig.js` | Property gate, financial year, billing-slab checks | checks and overrides values json cannot.
| `utils/tlPropertySearch.js` | Finds a property by ID or 10-digit mobile, and maps owners | 6,7,8,9 should be first letter and validation are taken care
| `utils/tlBillingSlab.js` | Fills unit of measure and the min/max quantity from billing slabs |
| `utils/tlSessionAdapter.js` | Turns form answers into the shape review and submit already use |
| `utils/useTlWizard.js` | Saves and clears the draft session |
| `pageComponents/TradeLicense.js` | Documents screen. Rows come from `RequiredDocuments.json` |
| `pages/citizen/Create/CheckPage.js` | Review |
| `pages/citizen/Create/TLAcknowledgement.js` | Creates the license and shows the result |

Keys named `_comment` or `_readme` in the JSON are notes. The screen ignores them.

## Behaviours that are not plain fields

**Property**

The citizen can search with a property ID or a 10-digit mobile number. Results are limited to their city. Picking a property stores it and fills the address. Owner name and mobile must be real text. 

Choosing **No** on “do you have a property?” shows a short message, then opens the property create screen. Changing building/movable clears any property already linked.

**Trade units and accessories**

Category, type, and subtype come from trade-type master data, filtered to Trade License (`type = TL`). When the citizen picks a subtype, the billing slab for that trade and building (or vehicle) fills the unit of measure and the allowed quantity range. If no slab exists, the field shows an error and they cannot continue with that combination. Accessories work the same way, using accessory slabs that have a rate. 

The license type sent to the server is always `PERMANENT` for this create flow.

**Owners**

“Same as property owner” copies gender and relationship from the property. “Same as trade address” copies the shop address into the owner’s permanent address, and keeps it in step if the trade address changes. Choosing No clears the copy.

**Documents**

The info screen only lists what to bring. The upload fields are in the third panel. 

## From answers to an application

1. The form keeps a flat list of answers.
2. On the way to review, `tlSessionAdapter.js` reshapes that list into the nested license the older check page and `convertToTrade` already expect. The flat answers stay the source of truth. 
3. The citizen confirms on the check page.
4. The acknowledgement screen calls the existing create API and shows the application number.



This document covers the citizen **new application**.
