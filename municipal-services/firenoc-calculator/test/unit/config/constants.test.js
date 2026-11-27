// generated-by: generate_node_unit_tests.py
describe('../../../src/config/constants', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/config/constants');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
