// generated-by: generate_node_unit_tests.py
describe('../../../src/api/update', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/api/update');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
