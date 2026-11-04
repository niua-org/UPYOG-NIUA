// generated-by: generate_node_unit_tests.py
describe('../../../src/api/search', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/api/search');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
