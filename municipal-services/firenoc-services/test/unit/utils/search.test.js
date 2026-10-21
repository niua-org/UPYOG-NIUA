// generated-by: generate_node_unit_tests.py
describe('../../../src/utils/search', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/utils/search');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
