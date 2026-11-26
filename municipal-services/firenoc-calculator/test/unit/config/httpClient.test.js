// generated-by: generate_node_unit_tests.py
describe('../../../src/config/httpClient', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/config/httpClient');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
