// generated-by: generate_node_unit_tests.py
describe('../../../src/model/fireNOC', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/model/fireNOC');
    }).not.toThrow();
    expect(mod).toBeDefined();
    // CommonJS export surface exercised via require above.
  });
});
