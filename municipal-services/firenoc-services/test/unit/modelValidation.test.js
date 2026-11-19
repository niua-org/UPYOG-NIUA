const { validateFireNOCSearchModel } = require('../../src/utils/modelValidation');

describe('validateFireNOCSearchModel', () => {
  test('validateFireNOCSearchModel_whenInvalidMobilePattern_returnsErrors', () => {
    const errors = validateFireNOCSearchModel({ tenantId: 'pg.citya', mobileNumber: 'abc' });
    expect(errors.length).toBeGreaterThan(0);
  });

  test('validateFireNOCSearchModel_whenValidTenantId_returnsNoErrors', () => {
    const errors = validateFireNOCSearchModel({ tenantId: 'pg.citya', pageNumber: 1 });
    expect(errors).toEqual([]);
  });
});
