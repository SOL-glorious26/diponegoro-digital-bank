import sys
root = sys.argv[1]
def rep(p, pairs):
    p = root + '/' + p
    s = open(p, encoding='utf8').read()
    for a, b in pairs:
        assert a in s, (p, a[:70])
        s = s.replace(a, b)
    open(p, 'w', encoding='utf8', newline='\n').write(s)

rep('account/data/LocalAccountRepository.kt', [
    ('        earlyWithdrawalConfirmed: Boolean = false, pin: String? = null', '        earlyWithdrawalConfirmed: Boolean, pin: String?'),
    ('    fun lockDescription(pocket: Pocket): String = when', '    override fun lockDescription(pocket: Pocket): String = when'),
    ('override suspend fun setBalanceHidden(userId: String, hidden: Boolean) = prefs.putBoolean(hiddenKey(userId), hidden)',
     'override suspend fun setBalanceHidden(userId: String, hidden: Boolean) { prefs.putBoolean(hiddenKey(userId), hidden) }'),
])

rep('account/presentation/AccountViewModels.kt', [
    ('viewModelScope.launch { CoreModule.ledger.revision.collect { load() } }', 'viewModelScope.launch { CoreModule.revision.collect { load() } }'),
    ('private fun load() = _state.update { it.copy(points = repo.balanceSeries(userId, it.period)) }',
     'private suspend fun load() {\n        val period = _state.value.period\n        val points = repo.balanceSeries(userId, period)\n        _state.update { if (it.period == period) it.copy(points = points) else it }\n    }'),
    ('        _state.update { it.copy(period = period) }\n        load()', '        _state.update { it.copy(period = period) }\n        viewModelScope.launch { load() }'),
    ('viewModelScope.launch { CoreModule.ledger.revision.collect { refresh() } }', 'viewModelScope.launch { CoreModule.revision.collect { refresh() } }'),
    ('        val overview = repo.overview(userId)\n        _state.update {\n            it.copy(\n                pockets = repo.pockets(userId),',
     '        val overview = repo.overview(userId)\n        val pockets = repo.pockets(userId)\n        _state.update {\n            it.copy(\n                pockets = pockets,'),
])
print('ok')
