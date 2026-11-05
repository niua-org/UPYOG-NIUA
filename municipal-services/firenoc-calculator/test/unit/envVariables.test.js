// generated-by: generate_node_unit_tests.py
describe('../../src/envVariables', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../src/envVariables');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
