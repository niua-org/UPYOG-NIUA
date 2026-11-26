// generated-by: generate_node_unit_tests.py
describe('../../../src/config/logger', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/config/logger');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
