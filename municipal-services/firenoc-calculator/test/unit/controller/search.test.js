// generated-by: generate_node_unit_tests.py
describe('../../../src/controller/search', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/controller/search');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
