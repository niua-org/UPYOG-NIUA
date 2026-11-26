// generated-by: generate_node_unit_tests.py
describe('../../../src/services/mdmsService', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/services/mdmsService');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
