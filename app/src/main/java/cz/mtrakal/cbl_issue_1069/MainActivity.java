package cz.mtrakal.cbl_issue_1069;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.initRepl)
    Button mInitRepl;
    @BindView(R.id.resolveConflicts)
    Button mResolveConflicts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @SuppressWarnings("unused") // it's actually used, just injected by Butter Knife
    @OnClick(R.id.initRepl)
    public void initReplClick() {
        App.getApp().startReplication();
    }

    @SuppressWarnings("unused") // it's actually used, just injected by Butter Knife
    @OnClick(R.id.resolveConflicts)
    public void resolveConflictsClick() {
        CouchBaseUtils.checkAndResolveConflicts();
    }
}
