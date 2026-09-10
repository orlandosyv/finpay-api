import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-feature-placeholder',
  templateUrl: './feature-placeholder.html',
  styleUrl: './feature-placeholder.scss',
})
export class FeaturePlaceholder {
  private readonly route = inject(ActivatedRoute);

  protected readonly heading = this.route.snapshot.data['heading'] as string;
  protected readonly description = this.route.snapshot.data['description'] as string;
  protected readonly nextCapability = this.route.snapshot.data['nextCapability'] as string;
}
