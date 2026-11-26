// generated-by: generate_node_unit_tests.py
describe('../../../src/services/userService', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/services/userService');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
