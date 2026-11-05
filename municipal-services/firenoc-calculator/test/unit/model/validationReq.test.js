// generated-by: generate_node_unit_tests.py
describe('../../../src/model/validationReq', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/model/validationReq');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
