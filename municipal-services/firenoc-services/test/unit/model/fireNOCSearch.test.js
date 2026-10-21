// generated-by: generate_node_unit_tests.py
describe('../../../src/model/fireNOCSearch', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/model/fireNOCSearch');
    }).not.toThrow();
    expect(mod).toBeDefined();
    // CommonJS export surface exercised via require above.
  });
});
