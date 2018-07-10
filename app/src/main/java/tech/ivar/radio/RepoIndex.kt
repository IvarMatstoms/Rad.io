package tech.ivar.radio

class RepoIndex {

}

private var _repoIndex:RepoIndex?=null;
fun getRepoIndex():RepoIndex {
    if (_repoIndex == null) {
        _repoIndex= RepoIndex()
    }
    return _repoIndex!!
}