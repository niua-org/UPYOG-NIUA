// generated-by: generate_node_unit_tests.py
describe('../../../src/services/firenocCalculatorService', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/services/firenocCalculatorService');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
