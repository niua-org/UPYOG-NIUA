# Form Workbench consumer

Generic consumer for workbench-generated form JSON.

**Contract:** [`design-refs/form-workbench-contract.md`](../../../../../../../design-refs/form-workbench-contract.md) (repo root)

## Exports

| Export | Role |
|--------|------|
| `resolveFormConfig` | Local vs MDMS master selection |
| `useFormWizard` | Session + steps + goNext |
| `FormFlowRoutes` | Route map + check/ack |
| `ConfigDrivenFormStep` | `accordion` → AccordionStep; else DynamicFormStep |
| `buildWizardSteps` / `isAccordionWizard` / `getNavigationPattern` | Pure step helpers |

`navigation.pattern` alone chooses layout (`accordion` | `oneStep` | `wizard`).
