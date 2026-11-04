// generated-by: generate_node_unit_tests.py
describe('../../../src/lib/util', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/lib/util');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
